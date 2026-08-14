import { z } from 'zod';

const foundationSchema = z.object({
  service: z.string(),
  version: z.string(),
});

export type FoundationServiceInfo = z.infer<typeof foundationSchema>;

export async function getFoundationInfo(): Promise<FoundationServiceInfo> {
  const response = await fetch('/api/foundation');

  if (!response.ok) {
    throw new Error(`Foundation request failed with status ${response.status}`);
  }

  return foundationSchema.parse(await response.json());
}

