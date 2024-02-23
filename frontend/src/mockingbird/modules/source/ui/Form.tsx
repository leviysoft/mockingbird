import React, { useCallback } from 'react';
import { useTranslation } from 'react-i18next';
import { useForm } from 'react-hook-form';
import { Button, Accordion } from '@mantine/core';
import { validateJSONArray } from 'src/mockingbird/infrastructure/helpers/forms';
import { Input } from 'src/mockingbird/components/form/Input';
import { InputJson } from 'src/mockingbird/components/form/InputJson';
import { mapSourceToFormData } from '../utils';
import type { Source, SourceFormData } from '../types';

type Props = {
  actions?: React.ReactElement;
  data?: Source;
  submitText?: string;
  disabled?: boolean;
  onSubmit: (data: SourceFormData) => void;
};

export default function Form(props: Props) {
  const { t } = useTranslation();
  const {
    actions,
    data,
    submitText = t('source.formSubmitTextDefault'),
    disabled = false,
    onSubmit: onSubmitParent,
  } = props;
  const defaultValues = mapSourceToFormData(data);
  const { control, handleSubmit } = useForm<SourceFormData>({
    defaultValues,
    mode: 'onBlur',
  });
  const onSubmit = useCallback(
    (formData: SourceFormData) => onSubmitParent(formData),
    [onSubmitParent]
  );
  return (
    <form onSubmit={handleSubmit(onSubmit)}>
      <Input
        name="name"
        label={t('source.nameLabel')}
        control={control as any}
        disabled={disabled || Boolean(data)}
        required
        mb="sm"
      />
      <Input
        name="description"
        label={t('source.descriptionLabel')}
        control={control as any}
        disabled={disabled}
        required
        mb="sm"
      />
      <InputJson
        name="request"
        label={t('source.requestLabel')}
        control={control as any}
        disabled={disabled}
        required
        mb="sm"
      />
      <InputJson
        name="init"
        label={t('source.initLabel')}
        control={control as any}
        disabled={disabled}
        validate={validateJSONArray}
        mb="sm"
      />
      <InputJson
        name="shutdown"
        label={t('source.shutdownLabel')}
        control={control as any}
        disabled={disabled}
        validate={validateJSONArray}
        mb="md"
      />
      <InputJson
        name="reInitTriggers"
        label={t('source.reInitTriggersLabel')}
        control={control as any}
        disabled={disabled}
        validate={validateJSONArray}
        mb="md"
      />
      {actions && (
        <Accordion variant="contained" mb="md">
          <Accordion.Item value="actions">
            <Accordion.Control>{t('source.actionsText')}</Accordion.Control>
            <Accordion.Panel>{actions}</Accordion.Panel>
          </Accordion.Item>
        </Accordion>
      )}
      <Button type="submit" size="md" disabled={disabled}>
        {submitText}
      </Button>
    </form>
  );
}
