import { NextResponse } from 'next/server';
import type { NextRequest } from 'next/server';

// The app is a client-side shell, but these paths must still render on a
// direct visit or refresh. Rewrite them internally while preserving the URL.
export function middleware(request: NextRequest) {
  return NextResponse.rewrite(new URL('/', request.url));
}

export const config = {
  matcher: ['/primeira-missao', '/for-you', '/avaliar', '/publicar', '/conta'],
};
