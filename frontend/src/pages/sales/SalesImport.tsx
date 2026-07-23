import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { salesApi } from "@/api/sales";
import { channelsApi } from "@/api/channels";
import { Button, Card, Select } from "@/components/ui";
import { Badge, ErrorState } from "@/components/states";
import { DownloadIcon, UploadIcon } from "@/components/icons";
import { apiErrorMessage, downloadAuthed } from "@/api/client";
import { ImportBatch } from "@/types/api";

const TEMPLATE_URL = `${import.meta.env.VITE_API_BASE_URL}/templates/sales-import-sample`;

export default function SalesImport() {
  const channels = useQuery({ queryKey: ["channels"], queryFn: () => channelsApi.list() });
  const [file, setFile] = useState<File | null>(null);
  const [channelId, setChannelId] = useState("");
  const [result, setResult] = useState<ImportBatch | null>(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  async function upload() {
    if (!file) return;
    setError("");
    setResult(null);
    setLoading(true);
    try {
      setResult(await salesApi.import(file, channelId || undefined));
    } catch (err) {
      setError(apiErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="max-w-2xl space-y-6">
      <Card className="space-y-5">
        <div className="space-y-2">
          <p className="text-sm text-slate-600">
            Upload a <b>.csv</b> or <b>.xlsx</b> with the following columns:
          </p>
          <code className="block overflow-x-auto rounded-lg border border-slate-200 bg-slate-50 px-3 py-2 font-mono text-xs text-slate-600">
            sku, channel, quantity, unitPrice, saleDate, marketplaceOrderId
          </code>
          <a
            href={TEMPLATE_URL}
            className="inline-flex items-center gap-1.5 text-sm font-medium text-brand-600 hover:text-brand-700"
          >
            <DownloadIcon className="text-base" /> Download sample template
          </a>
        </div>

        <Select
          label="Force all rows to a channel (optional — otherwise the 'channel' column is used)"
          value={channelId}
          onChange={(e) => setChannelId(e.target.value)}
        >
          <option value="">Use channel column from file</option>
          {channels.data?.map((c) => (
            <option key={c.id} value={c.id}>{c.name}</option>
          ))}
        </Select>

        <label className="flex cursor-pointer flex-col items-center justify-center gap-2 rounded-xl border-2 border-dashed border-slate-300 bg-slate-50/50 px-6 py-8 text-center transition hover:border-brand-400 hover:bg-brand-50/40">
          <UploadIcon className="text-2xl text-slate-400" />
          <span className="text-sm font-medium text-slate-700">
            {file ? file.name : "Choose a file to upload"}
          </span>
          <span className="text-xs text-slate-400">CSV or Excel</span>
          <input
            type="file"
            accept=".csv,.xlsx"
            onChange={(e) => setFile(e.target.files?.[0] ?? null)}
            className="hidden"
          />
        </label>

        {error && <ErrorState message={error} />}
        <Button onClick={upload} disabled={!file || loading}>
          <UploadIcon className="text-base" /> {loading ? "Uploading..." : "Upload & import"}
        </Button>
      </Card>

      {result && (
        <Card className="space-y-3">
          <h2 className="text-sm font-semibold text-slate-800">Import summary</h2>
          <div className="flex flex-wrap gap-2">
            <Badge>Total: {result.rowsTotal}</Badge>
            <Badge color="green">Success: {result.rowsSuccess}</Badge>
            <Badge color={result.rowsFailed > 0 ? "red" : "slate"}>Failed: {result.rowsFailed}</Badge>
            <Badge color={result.status === "COMPLETED" ? "green" : "amber"}>{result.status}</Badge>
          </div>
          {result.hasErrorReport && (
            <button
              onClick={() => downloadAuthed(`/import-batches/${result.id}/error-report`, `import-errors-${result.id}.csv`)}
              className="inline-flex items-center gap-1.5 text-sm font-medium text-brand-600 hover:text-brand-700"
            >
              <DownloadIcon className="text-base" /> Download error report for failed rows
            </button>
          )}
        </Card>
      )}
    </div>
  );
}
