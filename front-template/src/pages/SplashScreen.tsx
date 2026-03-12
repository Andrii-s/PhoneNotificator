import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { Phone } from "lucide-react";

const SplashScreen = () => {
  const navigate = useNavigate();
  const [fadeOut, setFadeOut] = useState(false);

  useEffect(() => {
    const timer1 = setTimeout(() => setFadeOut(true), 2000);
    const timer2 = setTimeout(() => navigate("/settings"), 2500);
    return () => {
      clearTimeout(timer1);
      clearTimeout(timer2);
    };
  }, [navigate]);

  return (
    <div
      className={`flex min-h-screen flex-col items-center justify-center bg-primary transition-opacity duration-500 ${fadeOut ? "opacity-0" : "opacity-100"}`}
    >
      <div className="mb-6 flex h-24 w-24 items-center justify-center rounded-2xl bg-primary-foreground/20 shadow-lg">
        <Phone className="h-14 w-14 text-primary-foreground" />
      </div>
      <h1 className="text-3xl font-bold text-primary-foreground">AutoDialer</h1>
      <p className="mt-2 text-sm text-primary-foreground/70">v1.0.0</p>
      <div className="mt-10 h-1 w-32 overflow-hidden rounded-full bg-primary-foreground/20">
        <div className="h-full animate-pulse rounded-full bg-primary-foreground/60" style={{ width: "60%" }} />
      </div>
    </div>
  );
};

export default SplashScreen;
