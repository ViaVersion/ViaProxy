/* hacks.js */

let hackEnabled = false;

let enchantments = {
    "protection": 4,
    "fire_protection": 4,
    "feather_falling": 4,
    "blast_protection": 4,
    "projectile_protection": 4,
    "respiration": 3,
    "aqua_affinity": 1,
    "thorns": 3,
    "depth_strider": 3,
    "sharpness": 5,
    "smite": 5,
    "bane_of_arthropods": 5,
    "knockback": 2,
    "fire_aspect": 2,
    "looting": 3,
    "efficiency": 5,
    "silk_touch": 1,
    "unbreaking": 3,
    "fortune": 3,
    "power": 5,
    "punch": 2,
    "flame": 1,
    "infinity": 1,
    "luck_of_the_sea": 3,
    "lure": 3
};

document.addEventListener('keydown', (event) => {
    if (event.key === 'i') {
        hackEnabled = !hackEnabled;
        alert(`Enchant hack ${hackEnabled ? 'enabled' : 'disabled'}`);
    }
});

document.addEventListener('keydown', (event) => {
    if (hackEnabled && event.key === 'e') {
        const selectedSlot = player.inventory.getSelected();
        if (selectedSlot) {
            for (const [enchantment, level] of Object.entries(enchantments)) {
                selectedSlot.enchantments.push({ id: enchantment, lvl: level });
            }
            alert(`Item in selected slot enchanted with: ${Object.keys(enchantments).join(', ')}`);
        }
    }
});

setInterval(() => {
    if (player) {
        player.hunger = 20;
    }
}, 1000);

setInterval(() => {
    if (player) {
        player.xp = 9999;
    }
}, 1000);
