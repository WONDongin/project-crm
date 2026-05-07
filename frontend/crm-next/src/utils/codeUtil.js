// src/utils/codeUtil.js

export const getCodeName = (codes, group, value) => {
  const target = codes[group]?.find((v) => v.code === value);

  return target?.name || value;
};
