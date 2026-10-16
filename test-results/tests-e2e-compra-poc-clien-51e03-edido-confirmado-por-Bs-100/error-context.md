# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: tests\e2e\compra-poc.spec.ts >> cliente compra dos poleras y ve su pedido confirmado por Bs 100
- Location: tests\e2e\compra-poc.spec.ts:3:5

# Error details

```
Error: page.goto: Protocol error (Page.navigate): Cannot navigate to invalid URL
Call log:
  - navigating to "/umss_ecommerce.html", waiting until "load"

```

# Test source

```ts
  1  | import { test, expect } from '@playwright/test';
  2  | 
  3  | test('cliente compra dos poleras y ve su pedido confirmado por Bs 100', async ({ page }) => {
> 4  |   await page.goto('/umss_ecommerce.html');
     |              ^ Error: page.goto: Protocol error (Page.navigate): Cannot navigate to invalid URL
  5  | 
  6  |   // Cada navegación inicializa el catálogo en memoria y el contexto es nuevo.
  7  |   // Esta compra y sus pagos se crean mediante la interfaz en esta ejecución.
  8  |   const carrito = page.getByTestId('carrito');
  9  |   const pedidos = page.getByTestId('pedidos-cliente');
  10 |   await expect(carrito).toHaveText('Tu carrito está vacío');
  11 |   await expect(pedidos.getByRole('heading', { name: /^Pedido / })).toHaveCount(0);
  12 | 
  13 |   const producto = page.getByRole('group', { name: 'Polera UMSS', exact: true });
  14 |   await producto.getByLabel('Cantidad de Polera UMSS', { exact: true }).fill('2');
  15 |   await producto.getByRole('button', { name: 'Agregar', exact: true }).click();
  16 |   await expect(carrito).toContainText('Polera UMSS');
  17 |   await expect(carrito).toContainText('2x Bs 50 = Bs 100');
  18 |   await expect(page.getByTestId('total-carrito')).toContainText('Bs 100');
  19 | 
  20 |   await page.getByLabel('Monto a Pagar (Bs):', { exact: true }).fill('100');
  21 |   await page.getByRole('button', { name: '💳 Pagar', exact: true }).click();
  22 | 
  23 |   await expect(pedidos.getByRole('heading', { name: /^Pedido / })).toHaveCount(1);
  24 |   await expect(pedidos.getByText('Productos: 2x Polera UMSS', { exact: true })).toBeVisible();
  25 |   await expect(pedidos.getByText('Total: Bs 100', { exact: true })).toBeVisible();
  26 |   await expect(pedidos.getByText('CONFIRMADO', { exact: true })).toBeVisible();
  27 |   await expect(carrito).toHaveText('Tu carrito está vacío');
  28 | });
  29 | 
```