import { expect, test } from '@playwright/test';

test('loads the app shell', async ({ page }) => {
  await page.goto('/');
  await expect(page.getByRole('heading', { name: /workflow operations console/i })).toBeVisible();
  await expect(page.getByRole('heading', { name: /build and run authenticated workflow dags/i })).toBeVisible();
});
