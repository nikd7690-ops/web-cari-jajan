import { getSession } from "@/lib/auth";
import BerandaPembeli from "@/components/BerandaPembeli";

export default async function HomePage() {
  const session = await getSession();

  return <BerandaPembeli loggedIn={Boolean(session?.user?.id)} />;
}