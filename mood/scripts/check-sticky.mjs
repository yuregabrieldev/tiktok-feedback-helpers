import { chromium } from "playwright-core";
import assert from "node:assert/strict";
const browser = await chromium.launch({
  executablePath: "/usr/bin/chromium",
  args: ["--no-sandbox"],
});
for (const width of [320, 390, 768, 1440]) {
  const page = await browser.newPage({ viewport: { width, height: 800 } });
  await page.goto("http://localhost:3000/");
  await page.evaluate(() => window.scrollTo(0, 1000));
  await page.waitForTimeout(200);
  const result = await page.evaluate(() => ({
    top: document.querySelector(".b-header").getBoundingClientRect().top,
    position: getComputedStyle(document.querySelector(".b-header")).position,
    scrollWidth: document.documentElement.scrollWidth,
  }));
  assert.equal(result.position, "fixed");
  assert.equal(result.top, 0);
  assert.ok(result.scrollWidth <= width);
  if (width < 1024) {
    await page.getByRole("button", { name: "Abrir menu" }).click();
    await page.keyboard.press("Escape");
    assert.equal(
      await page.getByRole("button", { name: "Abrir menu" }).getAttribute("aria-expanded"),
      "false",
    );
  }
  await page.close();
}
await browser.close();
console.log("Menu fixo e menu móvel validados nas quatro larguras.");
