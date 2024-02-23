import React, { useCallback, useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import type { BoxProps } from '@mantine/core';
import { Switch, Text, JsonInput, Box } from '@mantine/core';
import Copy from 'src/components/Copy/Copy';
import { stringifyJSON } from 'src/mockingbird/infrastructure/utils/forms';
import styles from './JSONRequest.css';

type Props = BoxProps & {
  getValues: () => Record<string, any> | Promise<string>;
};

export default function JSONRequest({ getValues, ...restProps }: Props) {
  const { t } = useTranslation();
  const [show, setShow] = useState(false);
  const handleChange = useCallback(
    (e) => setShow(e.currentTarget.checked),
    [setShow]
  );
  return (
    <Box {...restProps}>
      <div className={styles.header}>
        <Text size="md">{t('pages.mock.showAsJson')}</Text>
        <Switch
          label=""
          checked={show}
          onChange={handleChange}
          labelPosition="left"
        />
      </div>
      {show && (
        <div className={styles.description}>
          <Text color="grey" size="xs">
            {t('pages.mock.jsonRequestDescription')}
          </Text>
        </div>
      )}
      {show && <JSONViewer value={getValues()} />}
    </Box>
  );
}

function JSONViewer({ value }: { value: ReturnType<Props['getValues']> }) {
  const [v, setValue] = useState('');
  useEffect(() => {
    if (isPromise(value)) {
      value.then((val) => setValue(stringifyJSON(val)));
    } else {
      setValue(stringifyJSON(value));
    }
  }, [value, setValue]);
  return (
    <div className={styles.json}>
      <JsonInput
        value={v}
        rightSection={
          <Box sx={{ marginTop: '0.625rem', alignSelf: 'flex-start' }}>
            <Copy targetValue={v} />
          </Box>
        }
        minRows={16}
      />
    </div>
  );
}

function isPromise<T>(p: any): p is Promise<T> {
  return Boolean(p && typeof p.then === 'function');
}
