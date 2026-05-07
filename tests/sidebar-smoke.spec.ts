import { expect, test } from '@playwright/test'

const viewports = [
  { height: 900, name: 'desktop', width: 1280 },
  { height: 780, name: 'narrow', width: 420 },
  { height: 480, name: 'short', width: 900 },
] as const

test('sidebar opens and completed-task visibility is configurable', async ({
  page,
}) => {
  const consoleMessages: string[] = []

  page.on('console', (message) => {
    if (message.type() === 'error' || message.type() === 'warning') {
      consoleMessages.push(`${message.type()}: ${message.text()}`)
    }
  })

  page.on('pageerror', (error) => {
    consoleMessages.push(`pageerror: ${error.message}`)
  })

  await page.goto('/')
  await expect(page).toHaveTitle(/todobar/i)
  await expect(page.getByText('Todobar').first()).toBeVisible()
  await expect(page.getByText('Today').first()).toBeVisible()

  const openButton = page.getByRole('button', { name: 'Open Todobar' })

  if (await openButton.count()) {
    await openButton.click()
  }

  await page.getByRole('button', { name: 'Sidebar settings' }).click()
  await expect(page.getByRole('dialog', { name: 'Settings' })).toBeVisible()
  await expect(page.getByLabel('Show completed')).toBeVisible()

  await page.getByText('Show completed', { exact: true }).click()
  await expect(page.getByLabel('Show completed')).not.toBeChecked()
  await page.getByRole('button', { name: 'Close settings' }).click()
  await expect(page.getByText('Capture inbox', { exact: true })).toBeHidden()

  await page.getByRole('button', { name: 'Sidebar settings' }).click()
  await page.getByText('Show completed', { exact: true }).click()
  await expect(page.getByLabel('Show completed')).toBeChecked()
  await page.getByRole('button', { name: 'Close settings' }).click()
  await expect(page.getByText('Capture inbox', { exact: true })).toBeVisible()

  expect(consoleMessages).toEqual([])
})

for (const viewport of viewports) {
  test(`sidebar layout stays inside the viewport on ${viewport.name}`, async ({
    page,
  }) => {
    await page.setViewportSize({
      height: viewport.height,
      width: viewport.width,
    })
    await page.goto('/?open=1')

    const sidebar = page.locator('.todo-sidebar')
    const handle = page.locator('.edge-handle')

    await expect(sidebar).toBeVisible()
    await expect(handle).toBeVisible()
    await expect(page.getByRole('button', { name: 'Sidebar settings' })).toBeVisible()

    const sidebarBox = await sidebar.boundingBox()
    const handleBox = await handle.boundingBox()
    const documentWidth = await page.evaluate(
      () => document.documentElement.scrollWidth,
    )

    expect(sidebarBox).not.toBeNull()
    expect(handleBox).not.toBeNull()
    expect(documentWidth).toBeLessThanOrEqual(viewport.width + 2)

    if (sidebarBox && handleBox) {
      expect(sidebarBox.x).toBeGreaterThanOrEqual(-1)
      expect(sidebarBox.x + sidebarBox.width).toBeLessThanOrEqual(
        viewport.width + 1,
      )
      expect(handleBox.x).toBeGreaterThanOrEqual(-1)
      expect(handleBox.x + handleBox.width).toBeLessThanOrEqual(
        viewport.width + 1,
      )
      expect(handleBox.y).toBeGreaterThanOrEqual(-1)
      expect(handleBox.y + handleBox.height).toBeLessThanOrEqual(
        viewport.height + 1,
      )
    }

    await page.getByRole('button', { name: 'Sidebar settings' }).click()
    await expect(page.getByRole('dialog', { name: 'Settings' })).toBeVisible()
    await expect(page.getByText('Panel width', { exact: true })).toBeVisible()
  })
}
