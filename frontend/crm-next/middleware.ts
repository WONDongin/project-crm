// 권한 체크 (ADMIN / CONSULTANT 보호 라우팅)
import { NextRequest, NextResponse } from "next/server";

export function middleware(request: NextRequest) {
  const { pathname } = request.nextUrl;

  // 로그인 페이지는 통과
  if (pathname.startsWith("/login")) {
    return NextResponse.next();
  }

  // 보호할 경로
  if (pathname.startsWith("/admin") || pathname.startsWith("/consultant")) {
    const token = request.cookies.get("accessToken")?.value;
    if (!token) {
      // 토큰이 없으면 로그인 페이지로 리다이렉트
      const loginUrl = new URL("/login", request.url);
      return NextResponse.redirect(loginUrl);
    }
  }

  return NextResponse.next();
}

export const config = {
  matcher: ["/admin/:path*", "/consultant/:path*"],
};
