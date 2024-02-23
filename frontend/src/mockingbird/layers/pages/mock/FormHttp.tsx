import React, { useCallback, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useForm } from 'react-hook-form';
import { Flex, Box, Space, Button, Accordion } from '@mantine/core';
import { Input } from 'src/mockingbird/components/form/Input';
import InputCount from 'src/mockingbird/components/form/InputCount';
import InputSearchTagged from 'src/mockingbird/components/form/InputSearchTagged';
import Select from 'src/mockingbird/components/form/Select';
import { InputJson } from 'src/mockingbird/components/form/InputJson';
import ToggleBlock from 'src/mockingbird/components/form/ToggleBlock';
import type { THTTPMock } from 'src/mockingbird/models/mock/types';
import Callbacks from './Callbacks';
import JSONRequest from './JSONRequest';
import { mapStubToFormData, mapFormDataToStub } from './utils';
import { SCOPES, METHODS } from './refs';
import type { THTTPFormData, TFormCallback } from './types';

type Props = {
  labels: string[];
  serviceId: string;
  data?: THTTPMock;
  actions?: React.ReactElement;
  submitText?: string;
  submitDisabled?: boolean;
  onSubmit: (data: THTTPFormData, callbacks: TFormCallback[]) => void;
};

export default function FormHttp(props: Props) {
  const { t } = useTranslation();
  const {
    labels,
    serviceId = '',
    data,
    actions,
    submitText = t('pages.mock.formHttpSubmitTextDefault'),
    submitDisabled = false,
    onSubmit: onSubmitParent,
  } = props;
  const { callbacks: defaultCallbacks, ...defaultValues } = mapStubToFormData(
    serviceId,
    data
  );
  const [callbacks, setCallbacks] = useState<TFormCallback[]>(defaultCallbacks);
  const { control, watch, handleSubmit } = useForm<THTTPFormData>({
    defaultValues,
    mode: 'onBlur',
  });
  const scope = watch('scope');
  const onSubmit = useCallback(
    (formData: THTTPFormData) => onSubmitParent(formData, callbacks),
    [callbacks, onSubmitParent]
  );
  const onGetValues = useCallback(() => {
    return mapFormDataToStub(watch(), serviceId, callbacks);
  }, [watch, callbacks, serviceId]);
  return (
    <form onSubmit={handleSubmit(onSubmit)}>
      <Input
        name="name"
        label={t('pages.mock.nameLabel')}
        control={control as any}
        required
        mb="sm"
      />
      <InputSearchTagged
        name="labels"
        label={t('pages.mock.labelsLabel')}
        options={labels}
        control={control as any}
        mb="sm"
      />
      <Flex mb="sm">
        <Box sx={{ width: '50%' }}>
          <Select
            name="scope"
            label={t('pages.mock.scopeLabel')}
            options={SCOPES}
            control={control as any}
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
            />
          </Box>
        ) : (
          <Box sx={{ width: '50%' }} />
        )}
      </Flex>
      <Select
        name="method"
        label={t('pages.mock.methodNameLabel')}
        options={METHODS}
        control={control as any}
        required
        mb="sm"
      />
      <Flex mb="sm" align="flex-start">
        <Input
          name="path"
          label={t('pages.mock.pathLabel')}
          control={control as any}
          required
          description={`Без префикса /${serviceId}. Пример: /demo`}
        />
        <ToggleBlock
          name="isPathPattern"
          label={t('pages.mock.isPathPatternLabel')}
          control={control as any}
          mb="xs"
          ml="md"
          mt="3.125rem"
        />
      </Flex>
      <InputJson
        name="request"
        label={t('pages.mock.requestLabel')}
        control={control as any}
        required
        mb="sm"
      />
      <InputJson
        name="response"
        label={t('pages.mock.responseLabel')}
        control={control as any}
        required
        mb="sm"
      />
      <InputJson
        name="state"
        label={t('pages.mock.stateLabel')}
        control={control as any}
        mb="sm"
      />
      <InputJson
        name="persist"
        label={t('pages.mock.persistLabel')}
        control={control as any}
        mb="sm"
      />
      <InputJson
        name="seed"
        label={t('pages.mock.seedLabel')}
        control={control as any}
        mb="sm"
      />
      <Callbacks
        serviceId={serviceId}
        callbacks={callbacks}
        onChange={setCallbacks}
        mb="md"
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
      <Button type="submit" size="md" disabled={submitDisabled}>
        {submitText}
      </Button>
    </form>
  );
}
