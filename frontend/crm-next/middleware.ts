// 권한 체크 (ADMIN / CONSULTANT 보호 라우팅
import { NextRequest, NextResponse } from "next/server";

export function middleware(request: NextRequest) {
  return NextResponse.next();
}
