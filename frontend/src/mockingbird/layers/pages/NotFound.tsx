import React from 'react';
import { Button } from '@mantine/core';
import { useNavigate } from '@tramvai/module-router';
import PageHeader from 'src/components/PageHeader/PageHeader';
import { useTranslation } from 'react-i18next';

export default function NotFound() {
  const { t } = useTranslation();
  const navigateTo = useNavigate('/');
  return (
    <div>
      <PageHeader title={t('pages.notFound')} />
      <Button variant="outline" size="md" onClick={navigateTo}>
        {t('pages.goBack')}
      </Button>
    </div>
  );
}
