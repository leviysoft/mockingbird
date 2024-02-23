import type { ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import React, { useCallback } from 'react';
import { useForm } from 'react-hook-form';
import {
  Box,
  Flex,
  Space,
  Title,
  Button,
  Textarea,
  Accordion,
  Group,
} from '@mantine/core';
import AttachFile from 'src/mockingbird/components/form/AttachFile';
import { Input } from 'src/mockingbird/components/form/Input';
import InputCount from 'src/mockingbird/components/form/InputCount';
import InputSearchTagged from 'src/mockingbird/components/form/InputSearchTagged';
import Select from 'src/mockingbird/components/form/Select';
import { InputJson } from 'src/mockingbird/components/form/InputJson';
import type { TGRPCMock } from 'src/mockingbird/models/mock/types';
import JSONRequest from './JSONRequest';
import { mapGrpcToFormData, mapFormDataToGrpc } from './utils';
import { SCOPES } from './refs';
import type { TGRPCFormData } from './types';

type Props = {
  labels: string[];
  serviceId: string;
  data?: TGRPCMock;
  actions?: ReactNode;
  submitText?: string;
  submitDisabled?: boolean;
  disabled?: boolean;
  onSubmit: (data: TGRPCFormData) => void;
};

export default function FormGrpc(props: Props) {
  const { t } = useTranslation();
  const {
    labels,
    serviceId = '',
    data,
    actions,
    submitText = t('pages.mock.grpcSubmitTextDefault'),
    submitDisabled = false,
    disabled = false,
    onSubmit: onSubmitParent,
  } = props;
  const defaultValues = mapGrpcToFormData(serviceId, data);
  const { control, watch, handleSubmit } = useForm<TGRPCFormData>({
    defaultValues,
    mode: 'onBlur',
  });
  const scope = watch('scope');
  const onSubmit = useCallback(
    (formData: TGRPCFormData) => onSubmitParent(formData),
    [onSubmitParent]
  );
  const onGetValues = useCallback(() => {
    return mapFormDataToGrpc(watch(), serviceId);
  }, [watch, serviceId]);
  return (
    <form onSubmit={handleSubmit(onSubmit)}>
      <Input
        name="name"
        label={t('pages.mock.nameLabel')}
        control={control as any}
        disabled={disabled}
        required
        mb="sm"
      />
      <InputSearchTagged
        name="labels"
        label={t('pages.mock.labelsLabel')}
        options={labels}
        control={control as any}
        disabled={disabled}
        mb="sm"
      />
      <Flex mb="sm">
        <Box sx={{ width: '50%' }}>
          <Select
            name="scope"
            label={t('pages.mock.scopeLabel')}
            options={SCOPES}
            control={control as any}
            disabled={disabled}
            required
          />
        </Box>
        <Space w="md" />
        {scope === 'countdown' ? (
          <Box sx={{ width: '50%' }}>
            <InputCount
              name="times"
              label={t('pages.mock.timesLabel')}
              min={1}
              control={control as any}
              disabled={disabled}
            />
          </Box>
        ) : (
          <Box sx={{ width: '50%' }} />
        )}
      </Flex>
      <Input
        name="methodName"
        label={t('pages.mock.methodNameLabel')}
        control={control as any}
        disabled={disabled}
        required
        mb="sm"
      />
      <Title order={4}>{t('pages.mock.grpcRequestTitle')}</Title>
      {!disabled && (
        <AttachFile
          name="requestCodecs"
          label={t('pages.mock.requestCodecsLabel')}
          control={control as any}
          single
          required
          mb="sm"
        />
      )}
      {disabled && (
        <Textarea
          value={watch('requestSchema')}
          label={t('pages.mockrequestSchemaLabel')}
          minRows={15}
        />
      )}
      <Input
        name="requestClass"
        label={t('pages.mock.requestClassLabel')}
        control={control as any}
        disabled={disabled}
        required
        mb="sm"
      />
      <InputJson
        name="requestPredicates"
        label={t('pages.mock.requestPredicatesLabel')}
        control={control as any}
        disabled={disabled}
        required
        mb="sm"
      />
      <Title order={4}>Ответ</Title>
      {!disabled && (
        <AttachFile
          name="responseCodecs"
          label={t('pages.mock.responseCodecsLabel')}
          control={control as any}
          single
          required
          mb="sm"
        />
      )}
      {disabled && (
        <Textarea
          value={watch('responseSchema')}
          label={t('pages.mock.responseSchemaLabel')}
          minRows={15}
        />
      )}
      <Input
        name="responseClass"
        label={t('pages.mock.responseClassLabel')}
        control={control as any}
        disabled={disabled}
        required
        mb="sm"
      />
      <InputJson
        name="response"
        label={t('pages.mock.responseLabel')}
        control={control as any}
        disabled={disabled}
        required
        mb="sm"
      />
      <InputJson
        name="state"
        label={t('pages.mock.stateLabel')}
        control={control as any}
        disabled={disabled}
        mb="sm"
      />
      <InputJson
        name="seed"
        label={t('pages.mock.seedLabel')}
        control={control as any}
        disabled={disabled}
        mb="sm"
      />
      <JSONRequest getValues={onGetValues} mb="md" />
      {actions && (
        <Accordion variant="contained" mb="md">
          <Accordion.Item value="actions">
            <Accordion.Control>
              {t('pages.mock.actionsTitle')}
            </Accordion.Control>
            <Accordion.Panel>{actions}</Accordion.Panel>
          </Accordion.Item>
        </Accordion>
      )}
      <Group position="right">
        <Button type="submit" size="md" disabled={disabled || submitDisabled}>
          {submitText}
        </Button>
      </Group>
    </form>
  );
}
