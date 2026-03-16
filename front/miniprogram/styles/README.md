# Miniprogram Style Conventions

## Theme Token Source
- Global theme tokens are defined in `app.wxss` under `.theme-retro-*` blocks.
- Page `wxss` files should consume tokens using `var(--retro-...)`.

## Page Style Rule
- Page files keep only layout and structure styles.
- Do not duplicate per-theme selector blocks inside page files.
- If a new visual slot is needed, add a token in both themes first, then consume it in page styles.

## Add New Theme
1. Add theme metadata in `utils/theme.js`.
2. Add the same theme class token block in `app.wxss`.
3. No page-level theme style changes are required when tokens are complete.
