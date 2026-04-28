import React from 'react';
import DocSidebarItemLink from '@theme-original/DocSidebarItem/Link';

export default function DocSidebarItemLinkWrapper(props) {
  const badge = props.item?.customProps?.badge;
  if (!badge) {
    return <DocSidebarItemLink {...props} />;
  }
  return (
    <DocSidebarItemLink
      {...props}
      item={{
        ...props.item,
        label: (
          <>
            {props.item.label}
            <span className={`badge--${badge}`} />
          </>
        ),
      }}
    />
  );
}
