import React, { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { UnstyledButton, Menu, Group } from '@mantine/core';
import IconCheck from '@tabler/icons-react/dist/esm/icons/IconCheck';
import IconLanguage from '@tabler/icons-react/dist/esm/icons/IconLanguage';
import styles from './LanguagePicker.css';

type LanguageItem = { label: string; value: string };

const data: LanguageItem[] = [
  { label: 'English', value: 'en' },
  { label: 'Русский', value: 'ru' },
];

export function LanguagePicker() {
  const { i18n } = useTranslation();
  const [opened, setOpened] = useState(false);
  const [selected, setSelected] = useState(() => {
    return data.find((item) => item.value === i18n.language) || data[0];
  });
  const handleSelectLanguage = (item: LanguageItem) => {
    setOpened(false);
    setSelected(item);
    i18n.changeLanguage(item.value);
  };
  const items = data.map((item) => (
    <Menu.Item onClick={() => handleSelectLanguage(item)} key={item.label}>
      <Group className={styles.itemContent}>
        <span>{item.label}</span>
        {item.value === selected.value && <IconCheck className={styles.icon} />}
      </Group>
    </Menu.Item>
  ));

  return (
    <Menu
      onOpen={() => setOpened(true)}
      onClose={() => setOpened(false)}
      radius="md"
      width="target"
      withinPortal
    >
      <Menu.Target>
        <UnstyledButton
          className={styles.control}
          data-expanded={opened || undefined}
        >
          <IconLanguage color="grey" size="1.2rem" />
        </UnstyledButton>
      </Menu.Target>
      <Menu.Dropdown>{items}</Menu.Dropdown>
    </Menu>
  );
}
