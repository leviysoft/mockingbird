import React from 'react';
import { useTranslation } from 'react-i18next';
import IconLanguage from '@tabler/icons-react/dist/esm/icons/IconLanguage';
import { ActionIcon } from '@mantine/core';

type Props = {
  className?: string;
};

export function LanguageSwitcher({ className }: Props) {
  const { i18n } = useTranslation();
  const handleChange = () => {
    const nextLanguage = i18n.language === 'en' ? 'ru' : 'en';
    if (nextLanguage === i18n.language) return;
    i18n.changeLanguage(nextLanguage);
  };
  return (
    <ActionIcon className={className} onClick={handleChange}>
      <IconLanguage color="grey" size="1.2rem" />
    </ActionIcon>
  );
}
