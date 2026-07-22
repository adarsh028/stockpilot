import { useState } from "react";
import { Link } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { salesApi } from "@/api/sales";
import { channelsApi } from "@/api/channels";
import { Button, Card, Select } from "@/components/ui";
import { Badge, ErrorState } from "@/components/states";
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
    <div className="mx-auto max-w-2xl space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-slate-800">Import sales</h1>
        <Link to="/sales" className="text-sm text-brand-600 hover:underline">
          ← Back to sales
        </Link>
      </div>

      <Card className="space-y-4">
        <p className="text-sm text-slate-600">
          Upload a <b>.csv</b> or <b>.xlsx</b> with columns:{" "}
          <code className="rounded bg-slate-100 px-1 text-xs">
            sku, channel, quantity, unitPrice, saleDate, marketplaceOrderId
          </code>
        </p>
        <a href={TEMPLATE_URL} className="inline-block text-sm text-brand-600 hover:underline">
          ⬇ Download sample template
        </a>

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

        <input
          type="file"
          accept=".csv,.xlsx"
          onChange={(e) => setFile(e.target.files?.[0] ?? null)}
          className="block w-full text-sm text-slate-600 file:mr-3 file:rounded-md file:border-0 file:bg-brand-600 file:px-4 file:py-2 file:text-white"
        />

        {error && <ErrorState message={error} />}
        <Button onClick={upload} disabled={!file || loading}>
          {loading ? "Uploading..." : "Upload & import"}
        </Button>
      </Card>

      {result && (
        <Card className="space-y-2">
          <h2 className="font-semibold text-slate-700">Import summary</h2>
          <div className="flex flex-wrap gap-3 text-sm">
            <Badge>Total: {result.rowsTotal}</Badge>
            <Badge color="green">Success: {result.rowsSuccess}</Badge>
            <Badge color={result.rowsFailed > 0 ? "red" : "slate"}>Failed: {result.rowsFailed}</Badge>
            <Badge color={result.status === "COMPLETED" ? "green" : "amber"}>{result.status}</Badge>
          </div>
          {result.hasErrorReport && (
            <button
              onClick={() => downloadAuthed(`/import-batches/${result.id}/error-report`, `import-errors-${result.id}.csv`)}
              className="text-left text-sm text-brand-600 hover:underline"
            >
              ⬇ Download error report for failed rows
            </button>
          )}
        </Card>
      )}
    </div>
  );
}
