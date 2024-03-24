import React from 'react';
import { useTranslation } from 'react-i18next';
import { Text } from '@mantine/core';

interface Props {
  text?: string;
}

export default function ListEmpty(props: Props) {
  const { t } = useTranslation();
  const { text = t('components.list.textDefault') } = props;
  return <Text size="sm">{text}</Text>;
}
