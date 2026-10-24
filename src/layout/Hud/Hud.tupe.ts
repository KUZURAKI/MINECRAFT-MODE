export type Slot = {
  icon: string;
  name: string;
  kind: "tool" | "pills" | "bandage" | "food" | "water";
  count?: number;
  durability?: number;
};

export const slots: Slot[] = [
  { icon: "⚔️", name: "Алмазный меч", kind: "tool", durability: 0.72 },
  { icon: "⛏️", name: "Железная кирка", kind: "tool", durability: 0.35 },
  { icon: "💊", name: "Таблетки", kind: "pills", count: 4 },
  { icon: "🩹", name: "Бинт", kind: "bandage", count: 3 },
  { icon: "🍖", name: "Жареная свинина", kind: "food", count: 12 },
  { icon: "🧃", name: "Вода", kind: "water", count: 5 },
  { icon: "🔥", name: "Факел", kind: "tool", count: 48 },
];
