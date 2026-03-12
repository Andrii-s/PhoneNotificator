import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { Upload, Play, Pause, SkipBack, SkipForward, Volume2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group";
import { Label } from "@/components/ui/label";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Slider } from "@/components/ui/slider";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";

interface AudioFile {
  id: string;
  name: string;
}

const mockFiles: AudioFile[] = [
  { id: "1", name: "повідомлення_борг.mp3" },
  { id: "2", name: "нагадування_оплата.mp3" },
  { id: "3", name: "попередження.wav" },
];

const SettingsPage = () => {
  const navigate = useNavigate();
  const [files] = useState<AudioFile[]>(mockFiles);
  const [selectedFile, setSelectedFile] = useState<string>("1");
  const [playingFile, setPlayingFile] = useState<string | null>(null);
  const [progress, setProgress] = useState([30]);

  const togglePlay = (id: string) => {
    setPlayingFile(playingFile === id ? null : id);
  };

  return (
    <div className="flex min-h-screen flex-col bg-background">
      {/* Header */}
      <div className="bg-primary px-4 py-4 shadow-md">
        <h1 className="text-lg font-bold text-primary-foreground">⚙️ Налаштування</h1>
      </div>

      <div className="flex flex-1 flex-col gap-4 p-4">
        {/* Upload button */}
        <Button className="w-full gap-2" size="lg">
          <Upload className="h-5 w-5" />
          Завантажити аудіофайл
        </Button>

        {/* File list */}
        <Card>
          <CardHeader className="pb-3">
            <CardTitle className="text-base">Завантажені файли</CardTitle>
          </CardHeader>
          <CardContent className="p-0">
            <RadioGroup value={selectedFile} onValueChange={setSelectedFile}>
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead className="w-12">Обрати</TableHead>
                    <TableHead>Назва файлу</TableHead>
                    <TableHead className="w-20 text-center">Дія</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {files.map((file) => (
                    <TableRow key={file.id}>
                      <TableCell>
                        <RadioGroupItem value={file.id} id={`file-${file.id}`} />
                      </TableCell>
                      <TableCell>
                        <Label htmlFor={`file-${file.id}`} className="cursor-pointer text-sm">
                          {file.name}
                        </Label>
                      </TableCell>
                      <TableCell className="text-center">
                        <Button
                          variant="ghost"
                          size="icon"
                          onClick={() => togglePlay(file.id)}
                          className="h-8 w-8"
                        >
                          {playingFile === file.id ? (
                            <Pause className="h-4 w-4" />
                          ) : (
                            <Play className="h-4 w-4" />
                          )}
                        </Button>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </RadioGroup>
          </CardContent>
        </Card>

        {/* Audio controls */}
        <Card>
          <CardContent className="p-4">
            <p className="mb-2 text-xs text-muted-foreground">
              Зараз обрано: {files.find((f) => f.id === selectedFile)?.name}
            </p>
            <Slider value={progress} onValueChange={setProgress} max={100} step={1} className="mb-3" />
            <div className="flex items-center justify-between text-xs text-muted-foreground">
              <span>0:45</span>
              <span>2:30</span>
            </div>
            <div className="mt-3 flex items-center justify-center gap-4">
              <Button variant="ghost" size="icon">
                <SkipBack className="h-5 w-5" />
              </Button>
              <Button size="icon" className="h-12 w-12 rounded-full">
                {playingFile ? <Pause className="h-6 w-6" /> : <Play className="h-6 w-6" />}
              </Button>
              <Button variant="ghost" size="icon">
                <SkipForward className="h-5 w-5" />
              </Button>
              <Button variant="ghost" size="icon">
                <Volume2 className="h-5 w-5" />
              </Button>
            </div>
          </CardContent>
        </Card>

        {/* Next button */}
        <Button size="lg" className="mt-auto w-full" onClick={() => navigate("/debtors")}>
          Далі →
        </Button>
      </div>
    </div>
  );
};

export default SettingsPage;
