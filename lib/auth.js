import { getServerSession } from "next-auth";
import CredentialsProvider from "next-auth/providers/credentials";
import bcrypt from "bcryptjs";
import { connectDB } from "@/lib/mongodb";
import User from "@/models/User";

export const authOptions = {
  session: { strategy: "jwt" },
  pages: { signIn: "/masuk" },
  providers: [
    CredentialsProvider({
      name: "Credentials",
      credentials: {
        email: { label: "Email", type: "email" },
        password: { label: "Password", type: "password" },
      },
      async authorize(credentials) {
        if (!credentials?.email || !credentials?.password) {
          return null;
        }

        await connectDB();

        const user = await User.findOne({
          email: credentials.email.trim().toLowerCase(),
        });

        if (!user) {
          return null;
        }

        const cocok = await bcrypt.compare(
          credentials.password,
          user.passwordHash
        );

        if (!cocok) {
          return null;
        }

        if (user.isBanned) {
          throw new Error("Akun Anda telah ditangguhkan/diblokir oleh admin.");
        }

        return {
          id: user._id.toString(),
          name: user.name,
          email: user.email,
          role: user.role || "user",
          tipeAkun: user.tipeAkun || "pengguna",
        };
      },
    }),
  ],
  callbacks: {
    async jwt({ token, user }) {
      if (user) {
        token.id = user.id;
        token.role = user.role || "user";
        token.tipeAkun = user.tipeAkun || "pengguna";
      }
      return token;
    },
    async session({ session, token }) {
      if (session.user) {
        session.user.id = token.id;
        session.user.role = token.role || "user";
        session.user.tipeAkun = token.tipeAkun || "pengguna";
      }
      return session;
    },
  },
};

export function getSession() {
  return getServerSession(authOptions);
}