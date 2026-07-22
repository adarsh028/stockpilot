import { Link } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { salesApi } from "@/api/sales";
import { Card } from "@/components/ui";
import { Badge, EmptyState, ErrorState, LoadingSpinner } from "@/components/states";
import { apiErrorMessage, downloadAuthed } from "@/api/client";

export default function ImportBatches() {
  const batches = useQuery({ queryKey: ["batches"], queryFn: () => salesApi.batches() });

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-slate-800">Import history</h1>
        <Link to="/sales" className="text-sm text-brand-600 hover:underline">
          ← Back to sales
        </Link>
      </div>

      <Card>
        {batches.isLoading ? (
          <LoadingSpinner />
        ) : batches.isError ? (
          <ErrorState message={apiErrorMessage(batches.error)} />
        ) : batches.data && batches.data.content.length === 0 ? (
          <EmptyState title="No imports yet" />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-slate-100 text-left text-slate-500">
                  <th className="py-2">Date</th>
                  <th className="py-2">File</th>
                  <th className="py-2">Kind</th>
                  <th className="py-2 text-right">Total</th>
                  <th className="py-2 text-right">Success</th>
                  <th className="py-2 text-right">Failed</th>
                  <th className="py-2">Status</th>
                  <th className="py-2 text-right">Report</th>
                </tr>
              </thead>
              <tbody>
                {batches.data?.content.map((b) => (
                  <tr key={b.id} className="border-b border-slate-50">
                    <td className="py-2 text-slate-500">{new Date(b.createdAt).toLocaleString()}</td>
                    <td className="py-2 font-medium text-slate-700">{b.fileName}</td>
                    <td className="py-2 text-slate-500">{b.kind}</td>
                    <td className="py-2 text-right">{b.rowsTotal}</td>
                    <td className="py-2 text-right text-emerald-600">{b.rowsSuccess}</td>
                    <td className="py-2 text-right text-red-600">{b.rowsFailed}</td>
                    <td className="py-2">
                      <Badge color={b.status === "COMPLETED" ? "green" : b.status === "FAILED" ? "red" : "amber"}>
                        {b.status}
                      </Badge>
                    </td>
                    <td className="py-2 text-right">
                      {b.hasErrorReport ? (
                        <button
                          onClick={() => downloadAuthed(`/import-batches/${b.id}/error-report`, `import-errors-${b.id}.csv`)}
                          className="text-brand-600 hover:underline"
                        >
                          Download
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
    </div>
  );
}
