import assert from "node:assert/strict";
import fs from "node:fs/promises";
import { chromium } from "playwright-core";
import AxeBuilder from "@axe-core/playwright";

const base = process.env.BASE_URL || "http://localhost:3000";
const output = "artifacts/browser";
await fs.mkdir(output, { recursive: true });
const browser = await chromium.launch({
  executablePath: process.env.CHROMIUM_EXECUTABLE || "/usr/bin/chromium",
  args: ["--no-sandbox"],
});
const context = await browser.newContext();
const page = await context.newPage();
const errors = [];
page.on("pageerror", (error) => errors.push(error.message));
const pages = [
  "/",
  "/mood",
  "/fisiologia",
  "/investigacao",
  "/atletas",
  "/candidatura",
  "/contactos",
  "/admin",
];
const details = [
  "/fisiologia/fisiologia-exemplo",
  "/fisiologia/recuperacao-exemplo",
  "/fisiologia/treino-exemplo",
  "/investigacao/artigo-exemplo",
  "/investigacao/tese-exemplo",
  "/investigacao/preprint-exemplo",
  "/investigacao/publicacao-exemplo",
];
const results = { routes: [], layouts: [], accessibility: [], keyboard: {} };
try {
  for (const path of [
    ...pages,
    "/proposta-b",
    "/candidatura/recebida",
    ...details,
    "/nao-existe",
    "/fisiologia/invalido",
    "/investigacao/invalido",
  ]) {
    const response = await page.goto(base + path);
    const status = response.status();
    assert.equal(status, path.includes("invalido") || path === "/nao-existe" ? 404 : 200, path);
    assert.equal(await page.locator("h1").count(), 1, `${path}: one h1`);
    assert.ok(await page.title(), `${path}: title`);
    if (status === 200) {
      assert.equal(await page.locator("link[rel=canonical]").count(), 1, `${path}: canonical`);
      assert.ok(await page.locator('meta[property="og:image"]').getAttribute("content"));
      const canonical = await page.locator("link[rel=canonical]").getAttribute("href");
      assert.equal(new URL(canonical, base).pathname, path === "/proposta-b" ? "/" : path);
    }
    if (["/admin", "/proposta-b", "/candidatura/recebida", ...details].includes(path)) {
      assert.match(response.headers()["x-robots-tag"] || "", /noindex/);
      assert.match(await page.locator("meta[name=robots]").getAttribute("content"), /noindex/);
    }
    results.routes.push({ path, status, title: await page.title() });
  }
  const sitemap = await context.request.get(base + "/sitemap.xml");
  assert.equal(sitemap.status(), 200);
  assert.match(sitemap.headers()["content-type"], /xml/);
  assert.match(await sitemap.text(), /<urlset/);
  assert.doesNotMatch(await sitemap.text(), /exemplo|admin|recebida|proposta-b/);
  const robots = await context.request.get(base + "/robots.txt");
  assert.equal(robots.status(), 200);
  assert.match(await robots.text(), /User-agent: \*/);
  for (const width of [320, 390, 768, 1440]) {
    await page.setViewportSize({ width, height: 900 });
    for (const path of pages) {
      await page.goto(base + path);
      await page.evaluate(() => document.fonts.ready);
      const layout = await page.evaluate(() => ({
        viewport: innerWidth,
        document: document.documentElement.scrollWidth,
        brokenImages: [...document.images].filter((i) => !i.complete || !i.naturalWidth).length,
      }));
      assert.ok(
        layout.document <= layout.viewport,
        `${path} overflows at ${width}: ${layout.document}`,
      );
      assert.equal(layout.brokenImages, 0, `${path}: images`);
      results.layouts.push({ path, width, ...layout });
      const axe = await new AxeBuilder({ page })
        .withTags(["wcag2a", "wcag2aa", "wcag21aa", "wcag22aa"])
        .analyze();
      results.accessibility.push({ path, width, violations: axe.violations });
      assert.deepEqual(
        axe.violations.map((v) => v.id),
        [],
        `${path} axe at ${width}`,
      );
      await page.screenshot({
        path: `${output}/${path === "/" ? "home" : path.slice(1)}-${width}.png`,
        fullPage: true,
      });
    }
  }
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto(base + "/");
  await page.keyboard.press("Tab");
  assert.equal(await page.locator(":focus").textContent(), "Saltar para o conteúdo");
  await page.keyboard.press("Enter");
  assert.equal(await page.locator(":focus").getAttribute("id"), "home-content");
  await page.getByRole("button", { name: "Abrir menu" }).focus();
  await page.keyboard.press("Enter");
  assert.ok(await page.getByRole("navigation", { name: "Navegação móvel" }).isVisible());
  await page.keyboard.press("Escape");
  assert.equal(await page.locator(":focus").getAttribute("aria-label"), "Abrir menu");
  results.keyboard.skipAndEscape = true;
  await page.goto(base + "/candidatura");
  await page.getByRole("button", { name: "Validar exemplo" }).click();
  assert.equal(await page.getByRole("status").count(), 0);
  assert.equal(await page.locator("[name=name]").evaluate((e) => e.validity.valid), false);
  await page.locator("[name=name]").fill("EXEMPLO DE TESTE");
  await page.locator("[name=email]").fill("teste@example.invalid");
  await page.locator("[name=sport]").fill("Exemplo");
  await page.locator("[name=message]").fill("Teste local, sem envio");
  await page.locator("[type=checkbox]").check();
  await page.getByRole("button", { name: "Validar exemplo" }).click();
  assert.match(await page.getByRole("status").innerText(), /Nenhuma candidatura/);
  await page.locator("[name=name]").fill("EXEMPLO ALTERADO");
  assert.equal(await page.getByRole("status").count(), 0, "clear stale validation");
  assert.ok(await page.getByRole("button", { name: "Enviar candidatura" }).isDisabled());
  await page.locator("[name=name]").focus();
  await page.keyboard.press("Tab");
  assert.equal(await page.locator(":focus").getAttribute("name"), "email");
  assert.notEqual(
    await page.locator(":focus").evaluate((e) => getComputedStyle(e).outlineStyle),
    "none",
  );
  await page.goto(base + "/investigacao");
  await page
    .getByRole("textbox", { name: "Pesquisar publicações por título" })
    .fill("zzzz-ausente");
  assert.ok(await page.getByRole("heading", { name: "Nenhuma publicação encontrada" }).isVisible());
  await page.getByRole("button", { name: "Limpar filtros", exact: true }).click();
  assert.equal(await page.locator("tbody tr").count(), 4);
  await page.goto(base + "/admin");
  for (const name of ["Candidaturas", "Gestão editorial", "Institucional", "Visão geral"]) {
    await page.getByRole("button", { name, exact: true }).click();
    assert.equal(await page.locator("h1").innerText(), name);
  }
  assert.deepEqual(errors, []);
  console.log(
    `Passed: ${results.routes.length} page URLs, ${results.layouts.length} layouts/axe checks, sitemap/robots, keyboard, form, filters and admin.`,
  );
} finally {
  await fs.writeFile(`${output}/results.json`, JSON.stringify({ ...results, errors }, null, 2));
  await browser.close();
}
