import { useNavigate } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

const ConfirmLogout = () => {
  const navigate = useNavigate();

  const handleConfirm = () => {
    // TODO: add real logout (clear auth/token) if needed
    navigate("/");
  };

  const handleCancel = () => {
    navigate("/settings");
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-background p-4">
      <Card className="w-full max-w-md">
        <CardHeader>
          <CardTitle>Підтвердження виходу</CardTitle>
        </CardHeader>
        <CardContent>
          <p className="mb-6 text-sm">Ви дійсно готові вийти?</p>
          <div className="flex justify-end gap-3">
            <Button variant="secondary" onClick={handleCancel}>
              Ні
            </Button>
            <Button variant="destructive" onClick={handleConfirm}>
              Так
            </Button>
          </div>
        </CardContent>
      </Card>
    </div>
  );
};

export default ConfirmLogout;
