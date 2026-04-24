"use client";

import { useCallback, useEffect, useRef, useState } from "react";

import { List, X } from "lucide-react";

import { cn } from "@/lib/utils";

type TocItem = {
  id: string;
  text: string;
  level: number;
  element: HTMLElement;
};

const CONTENT_SELECTOR = ".prose";
const HEADING_SELECTOR = "h1[id], h2[id], h3[id]";

export default function PostToc() {
  const [open, setOpen] = useState(false);
  const [items, setItems] = useState<TocItem[]>([]);
  const [activeElement, setActiveElement] = useState<HTMLElement | null>(null);
  const panelRef = useRef<HTMLDivElement>(null);
  const fabRef = useRef<HTMLButtonElement>(null);

  const collect = useCallback(() => {
    const root = document.querySelector(CONTENT_SELECTOR);
    if (!root) {
      setItems([]);
      return;
    }
    const headings = root.querySelectorAll<HTMLElement>(HEADING_SELECTOR);
    const next: TocItem[] = [];
    headings.forEach((h) => {
      const id = h.id;
      const text = (h.innerText || h.textContent || "").trim();
      const level = Number(h.tagName.slice(1));
      if (id && text) next.push({ id, text, level, element: h });
    });
    setItems((prev) => {
      if (
        prev.length === next.length &&
        prev.every(
          (p, i) => p.element === next[i].element && p.text === next[i].text,
        )
      ) {
        return prev;
      }
      return next;
    });
  }, []);

  useEffect(() => {
    collect();
    const root = document.querySelector(CONTENT_SELECTOR);
    if (!root) return;
    const obs = new MutationObserver(() => collect());
    obs.observe(root, { childList: true, subtree: true });
    return () => obs.disconnect();
  }, [collect]);

  useEffect(() => {
    if (items.length === 0) return;
    const observer = new IntersectionObserver(
      (entries) => {
        const visible = entries
          .filter((e) => e.isIntersecting)
          .sort(
            (a, b) => a.boundingClientRect.top - b.boundingClientRect.top,
          );
        if (visible.length > 0) {
          setActiveElement(visible[0].target as HTMLElement);
        }
      },
      { rootMargin: "-15% 0px -70% 0px", threshold: 0 },
    );
    items.forEach((it) => observer.observe(it.element));
    return () => observer.disconnect();
  }, [items]);

  useEffect(() => {
    if (!open) return;
    const onKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") setOpen(false);
    };
    const onPointerDown = (e: PointerEvent) => {
      const target = e.target as Node;
      if (panelRef.current?.contains(target)) return;
      if (fabRef.current?.contains(target)) return;
      setOpen(false);
    };
    window.addEventListener("keydown", onKey);
    window.addEventListener("pointerdown", onPointerDown);
    return () => {
      window.removeEventListener("keydown", onKey);
      window.removeEventListener("pointerdown", onPointerDown);
    };
  }, [open]);

  const handleJump = (it: TocItem) => {
    it.element.scrollIntoView({ behavior: "smooth", block: "start" });
    history.replaceState(null, "", `#${encodeURIComponent(it.id)}`);
    if (window.matchMedia("(max-width: 767px)").matches) {
      setOpen(false);
    }
  };

  if (items.length === 0) return null;

  return (
    <>
      <div
        ref={panelRef}
        role="navigation"
        aria-label="목차"
        aria-hidden={!open}
        className={cn(
          "fixed z-40 bg-background border shadow-xl",
          "transition-all duration-200 ease-out",
          "inset-x-2 bottom-20 rounded-xl",
          "md:inset-x-auto md:right-6 md:bottom-24 md:w-80",
          "max-h-[60vh] md:max-h-[65vh]",
          "flex flex-col",
          open
            ? "translate-y-0 opacity-100 pointer-events-auto"
            : "translate-y-3 opacity-0 pointer-events-none",
        )}
      >
        <div className="flex items-center justify-between px-4 py-3 border-b">
          <span className="text-sm font-semibold">목차</span>
          <button
            type="button"
            onClick={() => setOpen(false)}
            className="text-muted-foreground hover:text-foreground transition-colors"
            aria-label="목차 닫기"
          >
            <X className="h-4 w-4" />
          </button>
        </div>
        <nav className="overflow-y-auto overscroll-contain py-2">
          <ul className="flex flex-col">
            {items.map((it, idx) => (
              <li key={`${it.id}-${idx}`}>
                <button
                  type="button"
                  onClick={() => handleJump(it)}
                  className={cn(
                    "block w-full text-left text-sm leading-5",
                    "px-4 py-1.5 transition-colors",
                    "hover:bg-accent hover:text-accent-foreground",
                    activeElement === it.element &&
                      "bg-accent/60 text-primary font-medium",
                    it.level === 2 && "pl-7",
                    it.level === 3 && "pl-10 text-muted-foreground",
                  )}
                >
                  <span className="line-clamp-2">{it.text}</span>
                </button>
              </li>
            ))}
          </ul>
        </nav>
      </div>

      <button
        ref={fabRef}
        type="button"
        onClick={() => setOpen((o) => !o)}
        aria-label={open ? "목차 닫기" : "목차 열기"}
        aria-expanded={open}
        className={cn(
          "fixed bottom-6 right-6 z-50",
          "h-12 w-12 rounded-full shadow-lg",
          "bg-primary text-primary-foreground",
          "flex items-center justify-center",
          "hover:scale-105 active:scale-95",
          "transition-transform duration-150",
        )}
      >
        {open ? <X className="h-5 w-5" /> : <List className="h-5 w-5" />}
      </button>
    </>
  );
}
