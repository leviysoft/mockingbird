import { mapSelectItem } from 'src/mockingbird/infrastructure/helpers/forms';
import i18n from 'src/mockingbird/i18n';
import type { TFormCallbackMessage, TFormCallbackHTTP } from './types';

export const SCOPES = [
  {
    label: i18n.t('pages.mock.scopesPersistentLabel'),
    value: 'persistent',
  },
  {
    label: i18n.t('pages.mock.scopesEphemeralLabel'),
    value: 'ephemeral',
  },
  {
    label: i18n.t('pages.mock.scopesCountdownLabel'),
    value: 'countdown',
  },
];

export const METHODS = [
  'POST',
  'GET',
  'PATCH',
  'DELETE',
  'PUT',
  'HEAD',
  'OPTIONS',
].map(mapSelectItem);

export const DEFAULT_REQUEST = {
  mode: 'json',
  body: {},
  headers: {
    /* eslint-disable-next-line @typescript-eslint/naming-convention */
    'Content-Type': 'application/json',
  },
};
export const DEFAULT_RESPONSE = {
  mode: 'json',
  body: {},
  headers: {
    /* eslint-disable-next-line @typescript-eslint/naming-convention */
    'Content-Type': 'application/json',
  },
  code: '200',
};

export const DEFAULT_SCENARIO_INPUT = {
  mode: 'json',
  payload: {},
};

export const CALLBACK_TYPES = [
  {
    label: 'HTTP',
    value: 'http',
  },
  {
    label: 'Message',
    value: 'message',
  },
];

export const CALLBACK_HTTP_RESPONSE_TYPES = [
  {
    label: '-',
    value: '',
  },
  {
    label: 'JSON',
    value: 'json',
  },
  {
    label: 'XML',
    value: 'xml',
  },
];

const DEFAULT_CALLBACK_HTTP_REQUEST = {
  url: 'https://tinkoff.ru',
  method: 'POST',
  mode: 'no_body',
  headers: {},
};

const DEFAULT_CALLBACK_MESSAGE_OUTPUT = {
  mode: 'json',
  payload: {},
};

export const DEFAULT_CALLBACK_HTTP: Partial<TFormCallbackHTTP> = {
  type: 'http',
  request: JSON.stringify(DEFAULT_CALLBACK_HTTP_REQUEST, undefined, 2),
  responseMode: '',
  persist: JSON.stringify({}),
};

export const DEFAULT_CALLBACK_MESSAGE: Partial<TFormCallbackMessage> = {
  type: 'message',
  destination: '',
  output: JSON.stringify(DEFAULT_CALLBACK_MESSAGE_OUTPUT, undefined, 2),
};
