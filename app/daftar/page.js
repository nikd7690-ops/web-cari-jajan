import { redirect } from "next/navigation";
import { getSession } from "@/lib/auth";
import DaftarForm from "@/components/DaftarForm";

export default async function DaftarPage() {
  const session = await getSession();

  if (session?.user) {
    redirect("/dashboard");
  }

  return <DaftarForm />;
}