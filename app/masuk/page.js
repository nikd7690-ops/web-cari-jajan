import { redirect } from "next/navigation";
import { getSession } from "@/lib/auth";
import MasukForm from "@/components/MasukForm";

export default async function MasukPage() {
  const session = await getSession();

  if (session?.user) {
    redirect("/dashboard");
  }

  return <MasukForm />;
}