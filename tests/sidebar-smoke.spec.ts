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
  await expect(page.getByText('Notifications', { exact: true })).toBeVisible()

  await page.getByRole('button', { name: 'Theme preset' }).click()
  await expect(page.getByRole('option')).toHaveCount(6)
  await expect(page.getByRole('option', { name: 'Choose Graphite' })).toHaveCount(0)
  await page.getByRole('button', { name: 'Switch to dark mode' }).click()
  await page.getByRole('button', { name: 'Theme preset' }).click()
  await expect(page.getByRole('option')).toHaveCount(6)
  await page.getByRole('option', { name: 'Choose Graphite' }).click()
  await expect(page.locator('.workspace')).toHaveClass(/theme-dark/)
  await expect(page.locator('.workspace')).toHaveClass(/style-graphite/)
  await expect(page.getByText('Edge position', { exact: true })).toBeVisible()
  await expect(page.getByText('Middle', { exact: true })).toHaveCount(0)
  await expect(page.getByRole('button', { name: 'Dock bottom' })).toHaveCount(0)
  await page.getByRole('button', { name: 'Dock left' }).click()
  await expect(page.locator('.workspace')).toHaveClass(/dock-left/)
  await page.getByRole('button', { name: 'Dock top' }).click()
  await expect(page.locator('.workspace')).toHaveClass(/dock-top/)
  await page.getByRole('button', { name: 'Dock right' }).click()
  await expect(page.locator('.workspace')).toHaveClass(/dock-right/)
  await page.getByRole('button', { name: 'Move Calendar up' }).click()

  await page.getByText('Show completed', { exact: true }).click()
  await expect(page.getByLabel('Show completed')).not.toBeChecked()
  await page.getByRole('button', { name: 'Close settings' }).click()
  await expect(page.getByText('Capture inbox', { exact: true })).toBeHidden()
  await expect(page.locator('section[aria-labelledby="today-heading"]')).toBeVisible()
  await expect(page.locator('section[aria-labelledby="calendar-heading"]')).toHaveCount(0)

  await page.getByLabel('Add a task to Today').fill('Reminder smoke')
  await page
    .locator('section[aria-labelledby="today-heading"] .quick-add .reminder-toggle')
    .click()
  await page
    .getByRole('textbox', { name: 'Reminder time' })
    .fill('2026-05-09T09:30')
  await page
    .locator('section[aria-labelledby="today-heading"] .quick-add .submit-task')
    .click()
  await expect(
    page
      .locator('section[aria-labelledby="today-heading"]')
      .getByText('Reminder smoke', { exact: true }),
  ).toBeVisible()
  await page.getByRole('button', { name: 'Edit Reminder smoke' }).click()
  await page.getByRole('textbox', { name: 'Edit Reminder smoke' }).fill(
    'Renamed smoke',
  )
  await page.getByRole('textbox', { name: 'Edit Reminder smoke' }).press('Enter')
  await expect(
    page
      .locator('section[aria-labelledby="today-heading"]')
      .getByText('Renamed smoke', { exact: true }),
  ).toBeVisible()
  await expect(page.locator('#reminders-section')).toHaveCount(0)
  await expect(page.locator('.task-reminder').filter({ hasText: '09:30' })).toBeVisible()

  await page.getByRole('button', { name: 'Jump to Calendar' }).click()
  await expect(page.getByRole('button', { name: 'Jump to Calendar' })).toHaveAttribute(
    'aria-current',
    'true',
  )
  await expect(page.locator('section[aria-labelledby="calendar-heading"]')).toBeVisible()
  await expect(page.getByRole('grid')).toBeVisible()
  await expect(page.getByLabel('Add a task to selected calendar day')).toBeVisible()
  await page
    .getByLabel('Add a task to selected calendar day')
    .fill('Calendar smoke')
  await page
    .locator('section[aria-labelledby="calendar-heading"] .quick-add .submit-task')
    .click()
  await expect(page.getByText('Calendar smoke', { exact: true })).toBeVisible()
  await page
    .locator('section[aria-labelledby="calendar-heading"]')
    .getByRole('button', { name: 'Jump to today', exact: true })
    .click()
  await expect(page.locator('.calendar-grid button.is-selected')).toContainText(
    String(new Date().getDate()),
  )
  await expect(page.locator('section[aria-labelledby="today-heading"]')).toHaveCount(0)
  await page.getByRole('button', { name: 'Jump to Lists' }).click()
  await expect(page.getByRole('button', { name: 'Jump to Lists' })).toHaveAttribute(
    'aria-current',
    'true',
  )
  await expect(page.locator('section[aria-labelledby="lists-heading"]')).toBeVisible()
  await expect(page.locator('section[aria-labelledby="calendar-heading"]')).toHaveCount(0)
  await page.getByLabel('Create a custom list').fill('Workstream')
  await page.getByRole('button', { name: 'Create list' }).click()
  await page.getByRole('button', { name: 'Rename Workstream' }).click()
  await page.getByRole('textbox', { name: 'Rename Workstream' }).fill('Planner')
  await page.getByRole('textbox', { name: 'Rename Workstream' }).press('Enter')
  const customList = page.locator('.custom-list').filter({ hasText: 'Planner' })
  await customList.getByLabel('Add a task to Planner').fill('Pinned task')
  await customList.locator('.submit-task').click()
  await customList.getByRole('button', { name: 'Show Planner on Today' }).click()
  await page.getByRole('button', { name: 'Jump to Today' }).click()
  await expect(page.getByText('Today goals', { exact: true })).toBeVisible()
  await expect(
    page.locator('.today-goal-list-title strong').filter({ hasText: 'Planner' }),
  ).toBeVisible()
  await expect(page.getByText('Pinned task', { exact: true })).toBeVisible()

  await page.getByRole('button', { name: 'Sidebar settings' }).click()
  await page.getByText('Show completed', { exact: true }).click()
  await expect(page.getByLabel('Show completed')).toBeChecked()
  await page.getByRole('button', { name: 'Close settings' }).click()
  await page.getByRole('button', { name: 'Jump to Today' }).click()
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

test('native closed dock keeps a rounded tab shape', async ({ page }) => {
  await page.setViewportSize({ height: 900, width: 442 })
  await page.goto('/?runtime=tauri')

  const path = await page.locator('.native-dock-surface path').getAttribute('d')

  expect(path).toContain('C')
  expect(path).toContain('Q 42')
  await expect(page.locator('.edge-handle')).toHaveCSS(
    'border-top-left-radius',
    '16px',
  )
  await expect(page.locator('.edge-handle')).toHaveCSS(
    'border-top-right-radius',
    '0px',
  )
})

test('native left dock keeps the tab on the outside edge', async ({ page }) => {
  await page.setViewportSize({ height: 900, width: 442 })
  await page.goto('/?runtime=tauri&dock=left')

  const path = await page.locator('.native-dock-surface path').getAttribute('d')

  expect(path).toContain('C')
  expect(path).toContain('Q 392')
  await expect(page.locator('.edge-handle')).toHaveCSS(
    'border-top-left-radius',
    '0px',
  )
  await expect(page.locator('.edge-handle')).toHaveCSS(
    'border-top-right-radius',
    '16px',
  )
})

test('native top dock keeps a wider outside tab shape', async ({ page }) => {
  await page.setViewportSize({ height: 500, width: 900 })

  await page.goto('/?runtime=tauri&dock=top')
  await expect(page.locator('.native-dock-surface path')).toHaveAttribute(
    'd',
    /Q/,
  )
  await expect(page.locator('.edge-handle')).toHaveCSS(
    'border-top-left-radius',
    '0px',
  )
  await expect(page.locator('.edge-handle')).toHaveCSS(
    'border-bottom-left-radius',
    '16px',
  )
  await expect(page.locator('.todo-sidebar')).toHaveCSS('width', '580px')
})
