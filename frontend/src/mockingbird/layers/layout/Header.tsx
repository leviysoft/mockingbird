import React, { useState, useEffect } from 'react';
import { connect } from '@tramvai/state';
import { Container, Title, Flex } from '@mantine/core';
import { getJson } from 'src/infrastructure/request';
import { LanguageSwitcher } from 'src/mockingbird/components/Language';
import { Shadow } from './Shadow';

type Props = {
  assetsPrefix: string;
};

function Header({ assetsPrefix }: Props) {
  const [version, setVersion] = useState(null);
  useEffect(() => {
    getJson(`${assetsPrefix}version.json`)
      .then((res) => {
        if (res && res.version) setVersion(res.version);
      })
      .catch(() => null);
  }, [assetsPrefix]);
  const title = version ? `Mockingbird v${version}` : 'Mockingbird';
  return (
    <Shadow>
      <Container>
        <Flex py="sm" align="center">
          <Title order={2} mr="auto">
            {title}
          </Title>
          <LanguageSwitcher />
        </Flex>
      </Container>
    </Shadow>
  );
}

const mapProps = ({ environment: { ASSETS_PREFIX: assetsPrefix } }) => ({
  assetsPrefix,
});

export default connect([], mapProps)(Header);
