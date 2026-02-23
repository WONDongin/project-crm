// src/constants/roles.js
export const ROLES = {
  ADMIN: "ROLE_ADMIN",
  CONSULTANT: "ROLE_CONSULTANT",
};

export const ROLE_LABELS = {
  [ROLES.ADMIN]: "관리자",
  [ROLES.CONSULTANT]: "상담사",
};
export const ROLE_OPTIONS = Object.entries(ROLE_LABELS).map(
  ([value, label]) => ({
    value,
    label,
  }),
);

export const ROLE_VALUES = Object.values(ROLES);
export const DEFAULT_ROLE = ROLES.CONSULTANT;
export const HIGHEST_ROLE = ROLES.ADMIN;
export const LOWEST_ROLE = ROLES.CONSULTANT;
