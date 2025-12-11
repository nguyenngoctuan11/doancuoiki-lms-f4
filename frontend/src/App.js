import { Outlet } from "react-router-dom";
import NavBar from "./components/NavBar";
import Footer from "./components/Footer";
import SupportChatWidget from "./components/support/SupportChatWidget";

export default function App() {
  return (
    <div className="min-h-screen bg-white flex flex-col">
      <NavBar />
      <main className="flex-1">
        <Outlet />
      </main>
      <Footer />
      <SupportChatWidget />
    </div>
  );
}
