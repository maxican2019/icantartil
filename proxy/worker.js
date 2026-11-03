// Proxy tipis: menyimpan client_secret Quran Foundation di server, bukan di APK.
// Hanya meneruskan GET ke endpoint yang dipakai aplikasi.
const ALLOWED = /^\/content\/api\/v4\/(chapters|verses\/by_chapter\/\d+|resources\/sync)$/;
let cached = { token: null, exp: 0 };

const hosts = (env) =>
  env.QF_ENV === "production"
    ? { auth: "https://prelive-oauth2.quran.foundation", api: "https://apis-prelive.quran.foundation" }
    : { auth: "https://oauth2.quran.foundation", api: "https://apis.quran.foundation" };

async function getToken(env, force) {
  if (!force && cached.token && Date.now() < cached.exp - 60000) return cached.token;
  const r = await fetch(hosts(env).auth + "/oauth2/token", {
    method: "POST",
    headers: {
      Authorization: "Basic " + btoa(env.QF_CLIENT_ID + ":" + env.QF_CLIENT_SECRET),
      "Content-Type": "application/x-www-form-urlencoded",
    },
    body: "grant_type=client_credentials&scope=content",
  });
  if (!r.ok) throw new Error("token " + r.status);
  const j = await r.json();
  cached = { token: j.access_token, exp: Date.now() + j.expires_in * 1000 };
  return cached.token;
}

export default {
  async fetch(req, env) {
    if (req.method !== "GET") return new Response("Method not allowed", { status: 405 });
    if (env.APP_KEY && req.headers.get("x-app-key") !== env.APP_KEY) return new Response("Forbidden", { status: 403 });
    const url = new URL(req.url);
    if (!ALLOWED.test(url.pathname)) return new Response("Not found", { status: 404 });
    const call = async (force) =>
      fetch(hosts(env).api + url.pathname + url.search, {
        headers: { "x-auth-token": await getToken(env, force), "x-client-id": env.QF_CLIENT_ID },
      });
    try {
      let r = await call(false);
      if (r.status === 401) r = await call(true); // sesuai dokumentasi: ulangi sekali
      return new Response(r.body, {
        status: r.status,
        headers: { "content-type": r.headers.get("content-type") || "application/json", "cache-control": "no-store" },
      });
    } catch (e) {
      return new Response("Upstream error", { status: 502 });
    }
  },
};
