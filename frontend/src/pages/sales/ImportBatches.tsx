import { useQuery } from "@tanstack/react-query";
import { salesApi } from "@/api/sales";
import { Card } from "@/components/ui";
import { Badge, EmptyState, ErrorState, LoadingSpinner } from "@/components/states";
import { DownloadIcon, InboxIcon } from "@/components/icons";
import { apiErrorMessage, downloadAuthed } from "@/api/client";

export default function ImportBatches() {
  const batches = useQuery({ queryKey: ["batches"], queryFn: () => salesApi.batches() });

  return (
    <Card>
        {batches.isLoading ? (
          <LoadingSpinner />
        ) : batches.isError ? (
          <ErrorState message={apiErrorMessage(batches.error)} />
        ) : batches.data && batches.data.content.length === 0 ? (
          <EmptyState icon={<InboxIcon />} title="No imports yet" />
        ) : (
          <div className="overflow-x-auto">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Date</th>
                  <th>File</th>
                  <th>Kind</th>
                  <th className="text-right">Total</th>
                  <th className="text-right">Success</th>
                  <th className="text-right">Failed</th>
                  <th>Status</th>
                  <th className="text-right">Report</th>
                </tr>
              </thead>
              <tbody>
                {batches.data?.content.map((b) => (
                  <tr key={b.id}>
                    <td className="whitespace-nowrap tabular-nums">{new Date(b.createdAt).toLocaleString()}</td>
                    <td className="font-medium text-slate-800">{b.fileName}</td>
                    <td>{b.kind}</td>
                    <td className="text-right tabular-nums">{b.rowsTotal}</td>
                    <td className="text-right font-medium tabular-nums text-emerald-600">{b.rowsSuccess}</td>
                    <td className="text-right font-medium tabular-nums text-red-600">{b.rowsFailed}</td>
                    <td>
                      <Badge color={b.status === "COMPLETED" ? "green" : b.status === "FAILED" ? "red" : "amber"}>
                        {b.status}
                      </Badge>
                    </td>
                    <td className="text-right">
                      {b.hasErrorReport ? (
                        <button
                          onClick={() => downloadAuthed(`/import-batches/${b.id}/error-report`, `import-errors-${b.id}.csv`)}
                          className="inline-flex items-center gap-1.5 rounded-md px-2.5 py-1 text-sm font-medium text-brand-600 transition hover:bg-brand-50"
                        >
                          <DownloadIcon className="text-base" /> Download
                        </button>
                      ) : (
                        "—"
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
        </div>
      )}
    </Card>
  );
}
