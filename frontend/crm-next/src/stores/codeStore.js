import { create } from "zustand";

import { persist } from "zustand/middleware";

const useCodeStore = create(
  persist(
    (set) => ({
      codes: {},

      hydrated: false,

      setCodes: (codes) =>
        set({
          codes,
        }),

      setHydrated: (state) =>
        set({
          hydrated: state,
        }),
    }),
    {
      name: "common-code-storage",

      skipHydration: true,
    },
  ),
);

export default useCodeStore;
