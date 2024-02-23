import React, { useCallback, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { useForm } from 'react-hook-form';
import { useNavigate } from '@tramvai/module-router';
import { useActions, useStoreSelector } from '@tramvai/state';
import { Modal as Popup, Button, Group } from '@mantine/core';
import { addToastAction } from 'src/infrastructure/notifications';
import { selectorAsIs } from 'src/mockingbird/infrastructure/helpers/state';
import { Input } from 'src/mockingbird/components/form/Input';
import { getPathMocks } from 'src/mockingbird/paths';
import { createAction, resetCreateStateAction } from '../actions/createAction';
import createServiceStore from '../reducers/createStore';
import type { Service } from '../types';

interface Props {
  opened: boolean;
  onClose: () => void;
}

export default function CreatePopup({ opened, onClose }: Props) {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { status, id, errorMessage } = useStoreSelector(
    createServiceStore,
    selectorAsIs
  );
  const create = useActions(createAction);
  const resetCreateState = useActions(resetCreateStateAction);
  const addToast = useActions(addToastAction);
  useEffect(() => resetCreateState as any, [resetCreateState]);
  useEffect(() => {
    if (status === 'complete' && id) {
      addToast({
        type: 'success',
        title: t('services.createSuccess'),
        timer: 3000,
      });
      navigate(getPathMocks(id));
    } else if (status === 'error' && errorMessage) {
      addToast({
        type: 'error',
        title: t('services.createError'),
        children: errorMessage,
        timer: 5000,
      });
    }
  }, [t, status, id, errorMessage, addToast, navigate]);
  const { control, handleSubmit } = useForm<Service>({
    defaultValues: {
      name: '',
      suffix: '',
    },
    mode: 'onBlur',
  });
  const onSubmit = useCallback((data: Service) => create(data), [create]);
  return (
    <Popup
      size="md"
      opened={opened}
      onClose={onClose}
      title={t('services.title')}
    >
      <form onSubmit={handleSubmit(onSubmit)}>
        <Input
          name="name"
          label={t('services.nameLabel')}
          control={control as any}
          required
          mb="sm"
        />
        <Input
          name="suffix"
          label={t('services.suffixLabel')}
          control={control as any}
          required
          mb="sm"
        />
        <Group position="right">
          <Button type="submit" disabled={status === 'loading'}>
            {t('services.submitText')}
          </Button>
        </Group>
      </form>
    </Popup>
  );
}
