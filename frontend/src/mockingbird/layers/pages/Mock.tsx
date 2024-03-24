import React, { useCallback, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { useActions, useStoreSelector } from '@tramvai/state';
import { useUrl } from '@tramvai/module-router';
import { Button, Text, Loader, Paper, Title } from '@mantine/core';
import PageHeader from 'src/components/PageHeader/PageHeader';
import Error from 'src/components/List/ListError';
import { selectorAsIs } from 'src/mockingbird/infrastructure/helpers/state';
import useLabels from 'src/mockingbird/modules/labels';
import mockStore from 'src/mockingbird/models/mock/reducers/store';
import {
  fetchAction,
  updateAction,
  deleteAction,
  resetMockStateAction,
} from 'src/mockingbird/models/mock/actions';
import { getPathMocks } from 'src/mockingbird/paths';
import FormHttp from './mock/FormHttp';
import FormScenario from './mock/FormScenario';
import FormGrpc from './mock/FormGrpc';
import {
  mapFormDataToStub,
  mapFormDataToScenario,
  mapFormDataToGrpc,
} from './mock/utils';
import type {
  THTTPFormData,
  TFormCallback,
  TScenarioFormData,
  TGRPCFormData,
} from './mock/types';

const TYPES = ['http', 'scenario', 'grpc'];

export default function Mock() {
  const { t } = useTranslation();
  const url = useUrl();
  const serviceId = Array.isArray(url.query.service)
    ? url.query.service[0]
    : url.query.service;
  const mockId = Array.isArray(url.query.mock)
    ? url.query.mock[0]
    : url.query.mock;
  const type = Array.isArray(url.query.type)
    ? url.query.type[0]
    : url.query.type;
  const basePath = getPathMocks(serviceId);
  const labels = useLabels(serviceId);

  const { status, data: mock } = useStoreSelector(mockStore, selectorAsIs);
  const fetchMock = useActions(fetchAction);
  const updateMock = useActions(updateAction);
  const deleteMock = useActions(deleteAction);
  const resetState = useActions(resetMockStateAction);
  useEffect(() => resetState as any, [resetState]);
  useEffect(() => {
    fetchMock({
      id: mockId,
      type,
    });
  }, [mockId, type, fetchMock]);
  const handleFetchRetry = useCallback(() => {
    if (!process.env.BROWSER) return;
    fetchMock({
      id: mockId,
      type,
    });
  }, [mockId, type, fetchMock]);
  type TData = THTTPFormData | TScenarioFormData | TGRPCFormData;
  const onUpdate = useCallback(
    (data: TData, callbacks: TFormCallback[] = []) => {
      let mapFn = (d: any, _serviceId: string, _callbacks: TFormCallback[]) =>
        d;
      switch (type) {
        case 'http':
          mapFn = mapFormDataToStub;
          break;
        case 'scenario':
          mapFn = mapFormDataToScenario;
          break;
        case 'grpc':
          mapFn = mapFormDataToGrpc;
          break;
      }
      updateMock({
        id: mockId,
        type,
        data: mapFn(data, serviceId, callbacks),
      });
    },
    [serviceId, mockId, type, updateMock]
  );
  const onDelete = useCallback(() => {
    deleteMock({
      id: mockId,
      type,
      basePath,
    });
  }, [basePath, mockId, type, deleteMock]);

  if (!process.env.BROWSER)
    return (
      <div>
        <PageHeader
          title={t('pages.mock.mockHeaderTitleDefault')}
          backText={t('pages.mock.mockHeaderBackText')}
          backPath={basePath}
        />
      </div>
    );

  const pageHeader = (
    <PageHeader
      title={formatTitle(type, t)}
      backText={t('pages.mock.mockHeaderBackText')}
      backPath={basePath}
    />
  );
  const actions = (
    <Paper>
      <Title order={4}>{t('pages.mock.actionDeleteTitle')}</Title>
      <Text size="md" mb="lg">
        {t('pages.mock.actionDeleteWarning')}
      </Text>
      <Button
        size="md"
        variant="outline"
        disabled={status === 'deleting'}
        onClick={onDelete}
      >
        {t('pages.mock.actionDeleteText')}
      </Button>
    </Paper>
  );
  if (!TYPES.includes(type))
    return (
      <div>
        {pageHeader}
        <Text size="md" color="red">
          {t('pages.mock.mockUnknownType')}
        </Text>
      </div>
    );
  return (
    <div>
      {pageHeader}
      {status === 'loading' && <Loader size="lg" />}
      {status === 'error' && <Error onRetry={handleFetchRetry} />}
      {type === 'http' && mock && (
        <FormHttp
          labels={labels}
          serviceId={serviceId}
          data={mock}
          actions={actions}
          submitText={t('pages.mock.editText')}
          submitDisabled={status === 'updating'}
          onSubmit={onUpdate}
        />
      )}
      {type === 'scenario' && mock && (
        <FormScenario
          labels={labels}
          serviceId={serviceId}
          data={mock}
          actions={actions}
          submitText={t('pages.mock.editText')}
          submitDisabled={status === 'updating'}
          onSubmit={onUpdate}
        />
      )}
      {type === 'grpc' && mock && (
        <FormGrpc
          labels={labels}
          serviceId={serviceId}
          data={mock}
          actions={actions}
          submitText={t('pages.mock.editText')}
          submitDisabled
          disabled
          onSubmit={onUpdate}
        />
      )}
    </div>
  );
}

function formatTitle(type: string, t: any) {
  switch (type) {
    case 'http':
      return t('pages.mock.mockHeaderTitleHttp');
    case 'scenario':
      return t('pages.mock.mockHeaderTitleScenario');
    case 'grpc':
      return t('pages.mock.mockHeaderTitleGrpc');
    default:
      return t('pages.mock.mockHeaderTitleDefault');
  }
}
