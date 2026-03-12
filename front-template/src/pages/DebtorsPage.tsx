import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { ArrowLeft, PhoneCall, Download, Users } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { Label } from "@/components/ui/label";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";

type SendMode = "debtors" | "manual" | "list";

const mockDebtors = [
  "+380931231212",
  "+380671234567",
  "+380501112233",
  "+380991234567",
  "+380661231234",
];

const mockLog = [
  { time: "14:01", message: "+380931231212 — дзвінок виконано ✓" },
  { time: "14:02", message: "+380671234567 — не відповів ✗" },
  { time: "14:03", message: "+380501112233 — дзвінок виконано ✓" },
];

const DebtorsPage = () => {
  const navigate = useNavigate();
  const [mode, setMode] = useState<SendMode>("debtors");
  const [manualNumber, setManualNumber] = useState("");
  const [listNumbers, setListNumbers] = useState("+380931231212\n+380671234567\n+380501112233");

  return (
    <div className="flex min-h-screen flex-col bg-background">
      {/* Header */}
      <div className="bg-primary px-4 py-4 shadow-md">
        <h1 className="text-lg font-bold text-primary-foreground">📞 Боржники</h1>
      </div>

      <div className="flex flex-1 flex-col gap-4 p-4">
        {/* Mode selector */}
        <div>
          <Label className="mb-2 block text-sm font-medium">Режим відправки</Label>
          <Select value={mode} onValueChange={(v) => setMode(v as SendMode)}>
            <SelectTrigger>
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="debtors">Боржникам</SelectItem>
              <SelectItem value="manual">Ввести номер вручну</SelectItem>
              <SelectItem value="list">Введення номерів списком</SelectItem>
            </SelectContent>
          </Select>
        </div>

        {/* Panel 1: Debtors list */}
        {mode === "debtors" && (
          <Card>
            <CardHeader className="pb-3">
              <CardTitle className="text-base">Список боржників</CardTitle>
            </CardHeader>
            <CardContent className="space-y-3">
              <Button variant="outline" className="w-full gap-2">
                <Download className="h-4 w-4" />
                Отримати список боржників
              </Button>
              <ScrollArea className="h-48 rounded-md border">
                <div className="p-3">
                  {mockDebtors.map((phone, i) => (
                    <div
                      key={i}
                      className="flex items-center gap-3 border-b border-border py-2.5 last:border-0"
                    >
                      <Users className="h-4 w-4 text-muted-foreground" />
                      <span className="text-sm font-mono">{phone}</span>
                    </div>
                  ))}
                </div>
              </ScrollArea>
              <Button className="w-full gap-2" size="lg">
                <PhoneCall className="h-5 w-5" />
                Почати дзвінки
              </Button>
            </CardContent>
          </Card>
        )}

        {/* Panel 2: Manual number */}
        {mode === "manual" && (
          <Card>
            <CardHeader className="pb-3">
              <CardTitle className="text-base">Ввести номер вручну</CardTitle>
            </CardHeader>
            <CardContent className="space-y-3">
              <div>
                <Label htmlFor="phone" className="text-sm">
                  Номер боржника
                </Label>
                <Input
                  id="phone"
                  type="tel"
                  placeholder="+380931231212"
                  value={manualNumber}
                  onChange={(e) => setManualNumber(e.target.value)}
                  className="mt-1 font-mono"
                />
                <p className="mt-1 text-xs text-muted-foreground">
                  Формат: +380XXXXXXXXX
                </p>
              </div>
              <Button className="w-full gap-2" size="lg">
                <PhoneCall className="h-5 w-5" />
                Почати дзвінок
              </Button>
            </CardContent>
          </Card>
        )}

        {/* Panel 3: List input */}
        {mode === "list" && (
          <Card>
            <CardHeader className="pb-3">
              <CardTitle className="text-base">Введення номерів списком</CardTitle>
            </CardHeader>
            <CardContent className="space-y-3">
              <div>
                <Label htmlFor="phone-list" className="text-sm">
                  Список боржників
                </Label>
                <Textarea
                  id="phone-list"
                  placeholder={"+380931231212\n+380671234567\n+380501112233"}
                  value={listNumbers}
                  onChange={(e) => setListNumbers(e.target.value)}
                  className="mt-1 min-h-[160px] font-mono text-sm"
                />
                <p className="mt-1 text-xs text-muted-foreground">
                  Кожний номер з нової строчки
                </p>
              </div>
              <Button className="w-full gap-2" size="lg">
                <PhoneCall className="h-5 w-5" />
                Почати дзвінки
              </Button>
            </CardContent>
          </Card>
        )}

        {/* Execution log */}
        <Card>
          <CardHeader className="pb-3">
            <CardTitle className="text-base">Журнал виконання</CardTitle>
          </CardHeader>
          <CardContent className="p-0">
            <ScrollArea className="h-36">
              <div className="p-4">
                {mockLog.map((entry, i) => (
                  <div key={i} className="border-b border-border py-2 last:border-0">
                    <span className="mr-2 text-xs text-muted-foreground">[{entry.time}]</span>
                    <span className="text-sm">{entry.message}</span>
                  </div>
                ))}
              </div>
            </ScrollArea>
          </CardContent>
        </Card>

        {/* Back button */}
        <Button
          variant="outline"
          size="lg"
          className="mt-auto w-full gap-2"
          onClick={() => navigate("/settings")}
        >
          <ArrowLeft className="h-5 w-5" />
          Повернутись
        </Button>
      </div>
    </div>
  );
};

export default DebtorsPage;
