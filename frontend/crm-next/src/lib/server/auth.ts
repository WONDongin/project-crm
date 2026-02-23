import { cookies } from "next/headers";

export async function getCurrentUser() {
  const cookieStore = await cookies();
  const token = cookieStore.get("accessToken");

  if (!token) return null;

  const res = await fetch("http://localhost:8080/auth/me", {
    headers: {
      Cookie: `accessToken=${token.value}`,
    },
    cache: "no-store",
  });

  if (!res.ok) return null;

  return res.json();
}