# Custom Components

This directory contains the project-owned base UI components.
No third-party UI library is required.

## Components
- `retro-button`: themed button with size/variant/loading states.
- `retro-avatar`: avatar with image fallback slot text.
- `retro-input`: single-line input with required label mark, inline strength hint, and hold-to-reveal password eye.
- `retro-textarea`: multi-line textarea wrapper.
- `retro-empty`: empty-state block with optional action slot.
- `retro-loading`: loading spinner with optional text.
- `retro-swipe-cell`: swipe container for right-side action menus.

## Usage Rule
- Register globally in `app.json`.
- Keep business pages focused on layout/data, not low-level UI behavior.
