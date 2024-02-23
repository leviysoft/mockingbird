import React from 'react';
import { useTranslation } from 'react-i18next';
import { Text, Button } from '@mantine/core';

interface Props {
  text?: string;
  onRetry?: () => void;
}

export default function ListError(props: Props) {
  const { t } = useTranslation();
  const { text = t('components.list.loadError'), onRetry } = props;
  return (
    <Text size="sm" color="red">
      {text}
      {onRetry && (
        <Button variant="subtle" compact onClick={onRetry}>
          {t('components.list.tryAgain')}
        </Button>
      )}
    </Text>
  );
}
