type Slot = { icon: string; name: string; count?: number; durability?: number };

export const slots: Slot[] = [
  { icon: "⚔️", name: "Алмазный меч", durability: 0.72 },
  { icon: "⛏️", name: "Железная кирка", durability: 0.35 },
  { icon: "🍖", name: "Жареная свинина", count: 12 },
  { icon: "🔥", name: "Факел", count: 48 },
  { icon: "🧪", name: "Зелье скорости", count: 3 },
];
