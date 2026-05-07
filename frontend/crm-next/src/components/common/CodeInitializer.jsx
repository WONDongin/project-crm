"use client";

import { useEffect } from "react";

import { getCommonCodes } from "@/lib/api/common.api";

import useCodeStore from "@/stores/codeStore";

export default function CodeInitializer() {
  const { setCodes, setHydrated } = useCodeStore();

  useEffect(() => {
    const init = async () => {
      try {
        // persist 복원
        await useCodeStore.persist.rehydrate();

        console.log("persist 복원 완료");

        const currentCodes = useCodeStore.getState().codes;

        // 이미 codes 있으면 API 안탐
        if (Object.keys(currentCodes).length > 0) {
          console.log("기존 공통코드 사용");

          setHydrated(true);

          return;
        }

        // 최초 1회만 API 호출
        const res = await getCommonCodes();

        console.log("공통코드 preload =", res);

        if (res.success) {
          setCodes(res.data);
        }

        setHydrated(true);
      } catch (error) {
        console.error("공통코드 preload 실패", error);
      }
    };

    init();
  }, []);

  return null;
}
