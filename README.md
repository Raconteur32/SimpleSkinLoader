# Simple Skin Swapper

**Simple Skin Swapper** is a client-side Fabric mod for Minecraft 1.21.8+ that lets you switch between your local skin files on the fly — without restarting the game or navigating through external websites. Open a dynamic skin wheel with a keybind, hover over the skin you want, click, and you're done.

---

## Features

- **Skin wheel** — a radial menu that displays up to 10 of your local skins at once, opened with a configurable keybind
- **Skin carousel** — a classic list view for browsing skins more carefully, or for accessing skins that don't fit on the wheel when you have more than 10
- **Server command support** — automatically send a server command after applying a skin so your skin refreshes in real time for other players, without needing to reconnect
- **Per-server configuration** — each server can have its own command configured

---

## Disclaimer

This mod is heavily inspired by two existing mods:

- [**SkinSwapper**](https://modrinth.com/mod/skinswapper) by [cobrasrock](https://modrinth.com/user/cobrasrock) — the original skin swapping mod, no longer updated for recent Minecraft versions
- [**SkinShuffle**](https://modrinth.com/mod/skinshuffle) by [imb11](https://modrinth.com/organization/imb11) — the modern equivalent, and by far the most complete mod in this category

Simple Skin Swapper was created because some aspects of SkinShuffle's design did not fit my workflow, and because the ability to trigger a server-side skin refresh command was missing. SkinShuffle's approach to quick skin switching also relies on keybinds rather than a dynamic radial wheel.

> **This mod is not a replacement for SkinShuffle.** If SkinShuffle works for you, use it — it has more features and broader server compatibility. This mod exists to fill a specific gap.

---

## Limitations

- **Multiplayer only.** The mod is designed around the multiplayer experience and does not work in singleplayer worlds. Singleplayer support may be added in the future.
- **Server command required for real-time updates.** For other players to see your new skin without you reconnecting, your server must have a command that triggers a skin refresh (e.g. `/reloadskin`). Without it, you will need to rejoin the server.
- **Not compatible with the SkinShuffle Bridge plugin** for now. This may be added in the future.

---

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/installer/) and [Fabric API](https://modrinth.com/mod/fabric-api)
2. Drop the mod `.jar` into your `.minecraft/mods/` folder
3. Add your skin PNG files to `.minecraft/skins/`
4. Launch the game and bind the skin wheel key in Controls

---

## Configuration

Open the configuration screen via [ModMenu](https://modrinth.com/mod/modmenu) or from the carousel/wheel screen.

For each server you connect to, you can set a command to send after applying a skin. The server entry is created automatically on first connection with an empty command. Leave it empty to disable the feature for that server.

**Config file location:** `.minecraft/config/simpleskinswapper.json`

```json
{
  "serverCommands": {
    "play.example.com": "/reloadskin",
    "another-server.net": ""
  }
}
```

---

## Usage

Both the skin wheel and the skin carousel can be opened with a configurable keybind. You can set them in **Options → Controls**, under the *Simple Skin Swapper* category.

<center>
  <img src="https://cdn.modrinth.com/data/kWMT8Yql/images/2e84e4de8eb2def89ff945cf21a5a3b3c282cc10.png" alt="Keybinds screen">
</center>

**Skin wheel:** hold your keybind to open the wheel, hover over the skin you want, and **click** to apply it. Releasing the keybind without clicking simply closes the wheel — no skin will be changed.

**Skin carousel:** open it by pressing its keybind once, or through ModMenu. Browse your skins and click the *Apply* button on the skin you want to switch to. You can also reorder skins using the arrow buttons — the order is saved, and it determines which skins appear on the wheel (the first 10 in the list).

<center>
  <img src="https://cdn.modrinth.com/data/kWMT8Yql/images/0c6e406889f39359e3dbde4ad60e372ca6a214d3.png" alt="Skin carousel screen">
</center>

**Adding skins:** drop any PNG skin file into the `.minecraft/skins/` folder (or the `skins/` folder of your instance if you use a custom launcher). A shortcut button to open that folder is available directly in the carousel screen.

---

## Acknowledgements

- [**cobrasrock**](https://modrinth.com/user/cobrasrock) for creating [SkinSwapper](https://modrinth.com/mod/skinswapper) under an open license that allowed me to reuse parts of its logic
- [**imb11**](https://modrinth.com/organization/imb11) for creating [SkinShuffle](https://modrinth.com/mod/skinshuffle), an outstanding mod that remains the most fully-featured in its category — and the one I would be using if not for the specific features I was looking for

---

## License

[GNU Lesser General Public License v3.0 or later](https://www.gnu.org/licenses/lgpl-3.0.html)
