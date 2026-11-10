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

        return {
          id: user._id.toString(),
          name: user.name,
          email: user.email,
        };
      },
    }),
  ],
  callbacks: {
    async jwt({ token, user }) {
      if (user) {
        token.id = user.id;
      }
      return token;
    },
    async session({ session, token }) {
      if (session.user) {
        session.user.id = token.id;
      }
      return session;
    },
  },
};

export function getSession() {
  return getServerSession(authOptions);
}