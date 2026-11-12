import { NextResponse } from "next/server";
import { getSession } from "@/lib/auth";
import { userDariRequest } from "@/lib/authToken";

export async function GET(request) {
  let user = userDariRequest(request);

  if (!user) {
    const session = await getSession();
    if (session?.user) {
      user = session.user;
    }
  }

  return NextResponse.json({
    user: user
      ? { id: user.id, name: user.name ?? null, email: user.email ?? null }
      : null,
  });
}